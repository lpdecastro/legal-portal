import { useParams } from 'react-router'
import Placeholder from '../components/Placeholder'

function CaseDetailsPage() {
  const { id } = useParams()

  return (
    <Placeholder title="Case Details">
      <p>Case ID: {id}</p>
    </Placeholder>
  )
}

export default CaseDetailsPage
