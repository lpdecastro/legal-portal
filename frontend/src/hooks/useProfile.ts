import { useQuery } from '@tanstack/react-query'
import { fetchProfile } from '../api/profile'

export const profileQueryKey = ['profile'] as const

// The signed-in client's profile; reloaded from the API whenever the profile page opens.
export function useProfile() {
  return useQuery({ queryKey: profileQueryKey, queryFn: fetchProfile })
}
