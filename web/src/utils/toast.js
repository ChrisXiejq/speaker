import { ElMessage } from 'element-plus'
import 'element-plus/es/components/message/style/css'

const defaults = { duration: 4000, showClose: true }

export function toastError(message) {
  ElMessage.error({ message, ...defaults })
}

export function toastWarning(message) {
  ElMessage.warning({ message, ...defaults })
}

export function toastSuccess(message) {
  ElMessage.success({ message, ...defaults })
}
