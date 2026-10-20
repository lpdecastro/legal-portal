import { App, Button, Modal } from 'antd'
import { CloseCircleOutlined } from '@ant-design/icons'
import { useState } from 'react'
import { ApiError } from '../api/client'
import type { LegalCase } from '../api/cases'
import { useCloseCase } from '../hooks/useCloseCase'

interface CloseCaseButtonProps {
  caseId: string
  // Called with the case as returned by the API after a successful closure.
  onClosed: (legalCase: LegalCase) => void
  // Called when the shown case is out of date (already closed, deleted or inaccessible).
  onStale: () => void
}

function closeErrorMessage(status: number): string {
  if (status === 404) return 'This case no longer exists or you no longer have access to it.'
  if (status === 409) return 'This case has already been closed.'
  return "We couldn't close this case. Please try again."
}

// Closes a submitted case after a confirmation dialog; on success the page shows it as closed, on failure it stays
// unchanged.
function CloseCaseButton({ caseId, onClosed, onStale }: CloseCaseButtonProps) {
  const { message } = App.useApp()
  const { submit, closing } = useCloseCase()
  const [confirming, setConfirming] = useState(false)

  const handleClose = async () => {
    try {
      const closed = await submit(caseId)
      if (!closed) return
      setConfirming(false)
      message.success('Case closed.')
      onClosed(closed)
    } catch (error) {
      setConfirming(false)
      const status = error instanceof ApiError ? error.status : 0
      // 401: the session ended and the client is being sent to the login page.
      if (status === 401) return
      message.error(closeErrorMessage(status))
      if (status === 404 || status === 409) onStale()
    }
  }

  return (
    <>
      <Button icon={<CloseCircleOutlined />} loading={closing} onClick={() => setConfirming(true)}>
        Close Case
      </Button>
      <Modal
        open={confirming}
        title="Close this case?"
        okText="Close Case"
        cancelButtonProps={{ disabled: closing }}
        confirmLoading={closing}
        closable={!closing}
        mask={{ closable: !closing }}
        keyboard={!closing}
        onOk={handleClose}
        onCancel={() => setConfirming(false)}
      >
        A closed case can't be reopened.
      </Modal>
    </>
  )
}

export default CloseCaseButton
