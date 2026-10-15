import { Link } from 'react-router'
import Placeholder from '../components/Placeholder'
import { caseDetailsPath } from '../routes/paths'

function MyCasesPage() {
  return (
    <Placeholder title="My Cases">
      <Link to={caseDetailsPath('example')}>View example case</Link>
    </Placeholder>
  )
}

export default MyCasesPage
