<script setup lang="ts">
/**
 * ============================================
 *  入库管理页 — 候选人需要实现（任务1）
 * ============================================
 *
 * 需求：
 * 1. 表单：供应商名称 + 入库明细列表
 * 2. 每行明细：选择商品（下拉搜索）→ 选择仓库 → 选择库位 → 输入数量
 * 3. 支持添加/删除明细行
 * 4. 提交按钮（调用 createInboundOrder API）
 *
 * 建议使用 AI 协作完成此页面，参考 ProductsView.vue 的实现风格
 */
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createInboundOrder,
  getProducts,
  getWarehouses,
  getLocations,
  type Product,
  type Warehouse,
  type Location,
} from '@/api'

interface InboundRow {
  productId?: number
  warehouseId?: number
  locationCode: string
  quantity: number
  locations: Location[]
}

const supplierName = ref('')
const items = ref<InboundRow[]>([])
const submitting = ref(false)
const products = ref<Product[]>([])
const warehouses = ref<Warehouse[]>([])

const loadProducts = async (keyword?: string) => {
  const res = await getProducts(keyword)
  products.value = res.data
}

const loadWarehouses = async () => {
  const res = await getWarehouses()
  warehouses.value = res.data
}

onMounted(() => {
  loadProducts()
  loadWarehouses()
})

const addItem = () => {
  items.value.push({
    productId: undefined,
    warehouseId: undefined,
    locationCode: '',
    quantity: 1,
    locations: [],
  })
}

const removeItem = (index: number) => {
  items.value.splice(index, 1)
}

const onWarehouseChange = async (row: InboundRow) => {
  row.locationCode = ''
  row.locations = []
  if (!row.warehouseId) return
  const res = await getLocations(row.warehouseId)
  row.locations = res.data
}

const handleSubmit = async () => {
  if (!supplierName.value.trim()) {
    ElMessage.warning('请输入供应商名称')
    return
  }
  if (items.value.length === 0) {
    ElMessage.warning('请至少添加一条入库明细')
    return
  }
  for (const [i, row] of items.value.entries()) {
    if (!row.productId) {
      ElMessage.warning(`第 ${i + 1} 行：请选择商品`)
      return
    }
    if (!row.locationCode) {
      ElMessage.warning(`第 ${i + 1} 行：请选择库位`)
      return
    }
    if (!row.quantity || row.quantity < 1) {
      ElMessage.warning(`第 ${i + 1} 行：数量必须大于 0`)
      return
    }
  }

  submitting.value = true
  try {
    await createInboundOrder({
      supplierName: supplierName.value.trim(),
      items: items.value.map((row) => ({
        productId: row.productId!,
        quantity: row.quantity,
        locationCode: row.locationCode,
      })),
    })
    ElMessage.success('入库单创建成功')
    supplierName.value = ''
    items.value = []
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || e.message || '提交失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div>
    <h3>入库管理</h3>

    <el-form label-width="100px" style="max-width: 900px">
      <el-form-item label="供应商名称" required>
        <el-input v-model="supplierName" placeholder="请输入供应商名称" />
      </el-form-item>

      <el-form-item label="入库明细">
        <el-button type="primary" @click="addItem">+ 添加明细</el-button>
      </el-form-item>
    </el-form>

    <div
      v-for="(item, index) in items"
      :key="index"
      style="margin-bottom: 12px; display: flex; gap: 12px; align-items: center; flex-wrap: wrap"
    >
      <el-select
        v-model="item.productId"
        placeholder="选择商品"
        filterable
        remote
        :remote-method="loadProducts"
        style="width: 220px"
      >
        <el-option
          v-for="p in products"
          :key="p.id"
          :label="`${p.name} (${p.sku})`"
          :value="p.id"
        />
      </el-select>

      <el-select
        v-model="item.warehouseId"
        placeholder="选择仓库"
        style="width: 160px"
        @change="onWarehouseChange(item)"
      >
        <el-option
          v-for="wh in warehouses"
          :key="wh.id"
          :label="wh.name"
          :value="wh.id"
        />
      </el-select>

      <el-select
        v-model="item.locationCode"
        placeholder="选择库位"
        :disabled="!item.warehouseId"
        style="width: 160px"
      >
        <el-option
          v-for="loc in item.locations"
          :key="loc.code"
          :label="loc.code"
          :value="loc.code"
        />
      </el-select>

      <el-input-number v-model="item.quantity" :min="1" placeholder="数量" />

      <el-button type="danger" size="small" @click="removeItem(index)">删除</el-button>
    </div>

    <el-button
      type="success"
      :loading="submitting"
      @click="handleSubmit"
      :disabled="items.length === 0"
      style="margin-top: 8px"
    >
      提交入库单
    </el-button>

    <el-empty v-if="items.length === 0" description='请点击「添加明细」按钮添加入库商品' />
  </div>
</template>
