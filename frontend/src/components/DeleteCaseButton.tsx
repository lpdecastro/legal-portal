import { App, Button, Modal } from 'antd'
import { DeleteOutlined } from '@ant-design/icons'
import { useState } from 'react'
import { useNavigate } from 'react-router'
import { ApiError } from '../api/client'
import { useDeleteCase } from '../hooks/useDeleteCase'
import { paths } from '../routes/paths'

function deleteErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 404) return 'This case no longer exists or you no longer have access to it.'
    if (error.status === 409) return 'Only draft cases can be deleted.'
  }
  return "We couldn't delete this case. Please try again."
}

// Deletes a draft after a confirmation dialog; on success goes to My Cases, on failure stays on the page.
function DeleteCaseButton({ caseId }: { caseId: string }) {
  const { message } = App.useApp()
  const navigate = useNavigate()
  const { submit, deleting } = useDeleteCase()
  const [confirming, setConfirming] = useState(false)

  const handleDelete = async () => {
    try {
      if (!(await submit(caseId))) return
      message.success('Draft deleted.')
      navigate(paths.cases)
    } catch (error) {
      setConfirming(false)
      // 401: the session ended and the client is being sent to the login page.
      if (!(error instanceof ApiError && error.status === 401)) {
        message.error(deleteErrorMessage(error))
      }
    }
  }

  return (
    <>
      <Button danger icon={<DeleteOutlined />} loading={deleting} onClick={() => setConfirming(true)}>
        Delete
      </Button>
      <Modal
        open={confirming}
        title="Delete this draft?"
        okText="Delete"
        okButtonProps={{ danger: true }}
        cancelButtonProps={{ disabled: deleting }}
        confirmLoading={deleting}
        closable={!deleting}
        mask={{ closable: !deleting }}
        keyboard={!deleting}
        onOk={handleDelete}
        onCancel={() => setConfirming(false)}
      >
        This draft will be removed from My Cases. This can't be undone.
      </Modal>
    </>
  )
}

export default DeleteCaseButton
