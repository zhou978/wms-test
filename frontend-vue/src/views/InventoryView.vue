<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getInventory, getWarehouses, type InventoryItem, type Warehouse } from '@/api'

const keyword = ref('')
const warehouseId = ref<number | undefined>()
const loading = ref(false)
const inventoryList = ref<InventoryItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const warehouses = ref<Warehouse[]>([])

let debounceTimer: ReturnType<typeof setTimeout> | null = null

const loadInventory = async () => {
  loading.value = true
  try {
    const res = await getInventory({
      keyword: keyword.value || undefined,
      warehouseId: warehouseId.value,
      page: page.value,
      pageSize: pageSize.value,
    })
    inventoryList.value = res.data.list
    total.value = res.data.total
    page.value = res.data.page
    pageSize.value = res.data.pageSize
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const loadWarehouses = async () => {
  const res = await getWarehouses()
  warehouses.value = res.data
}

const debouncedSearch = () => {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    page.value = 1
    loadInventory()
  }, 300)
}

watch(keyword, debouncedSearch)

onMounted(async () => {
  await loadWarehouses()
  await loadInventory()
})

const getRowStyle = ({ row }: { row: InventoryItem }) => {
  if (row.quantity < 10) {
    return { color: '#f56c6c', fontWeight: 'bold' }
  }
  return {}
}

const onWarehouseChange = () => {
  page.value = 1
  loadInventory()
}
</script>

<template>
  <div>
    <h3>库存查询</h3>

    <div style="display: flex; gap: 12px; margin-bottom: 16px">
      <el-input
        v-model="keyword"
        placeholder="搜索商品名称/SKU..."
        style="width: 300px"
        clearable
      />
      <el-select
        v-model="warehouseId"
        placeholder="选择仓库"
        clearable
        style="width: 200px"
        @change="onWarehouseChange"
      >
        <el-option
          v-for="wh in warehouses"
          :key="wh.id"
          :label="wh.name"
          :value="wh.id"
        />
      </el-select>
      <el-button type="primary" @click="loadInventory">查询</el-button>
    </div>

    <el-table
      :data="inventoryList"
      v-loading="loading"
      border
      stripe
      :row-style="getRowStyle"
    >
      <el-table-column prop="productName" label="商品名称" />
      <el-table-column prop="sku" label="SKU" width="150" />
      <el-table-column prop="locationCode" label="库位编码" width="150" />
      <el-table-column prop="warehouseName" label="仓库" width="120" />
      <el-table-column prop="quantity" label="库存数量" width="100" />
      <el-table-column prop="updatedAt" label="更新时间" width="180" />
    </el-table>

    <div style="margin-top: 16px; text-align: right">
      <el-pagination
        v-model:current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadInventory"
      />
    </div>

    <el-empty
      v-if="!loading && inventoryList.length === 0"
      description="暂无库存数据，请先完成入库操作"
    />
  </div>
</template>
