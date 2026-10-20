import { useCallback, useRef, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { updateProfile } from '../api/profile'
import type { ClientProfile, UpdateProfileRequest } from '../api/profile'
import { useAuth } from './useAuth'
import { profileQueryKey } from './useProfile'

interface UseUpdateProfileResult {
  // Resolves `undefined` when ignored because a save is already in flight; errors propagate to the caller.
  submit: (request: UpdateProfileRequest) => Promise<ClientProfile | undefined>
  submitting: boolean
}

// Saves profile changes, then shows the saved profile on the page and the new name in the profile menu.
export function useUpdateProfile(): UseUpdateProfileResult {
  const [submitting, setSubmitting] = useState(false)
  // Blocks a second save that starts before the `submitting` re-render disables the form.
  const inFlightRef = useRef(false)
  const queryClient = useQueryClient()
  const { setUser, user } = useAuth()

  const submit = useCallback(
    async (request: UpdateProfileRequest) => {
      if (inFlightRef.current) return undefined
      inFlightRef.current = true
      setSubmitting(true)
      try {
        const saved = await updateProfile(request)
        queryClient.setQueryData(profileQueryKey, saved)
        if (user) setUser({ ...user, firstName: saved.firstName, lastName: saved.lastName })
        return saved
      } finally {
        inFlightRef.current = false
        setSubmitting(false)
      }
    },
    [queryClient, setUser, user],
  )

  return { submit, submitting }
}
