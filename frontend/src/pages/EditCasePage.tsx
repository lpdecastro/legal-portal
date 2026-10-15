import { useParams } from 'react-router'
import Placeholder from '../components/Placeholder'

function EditCasePage() {
  const { id } = useParams()

  return (
    <Placeholder title="Edit Draft">
      <p>Case ID: {id}</p>
    </Placeholder>
  )
}

export default EditCasePage
