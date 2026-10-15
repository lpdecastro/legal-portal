# Coding Standards

## TypeScript

- Strict mode enabled
- No `any` types - use proper typing or `unknown`
- Define interfaces for all props, API responses, and data models
- Use type inference where obvious, explicit types where helpful

# React

- Functional components only (no class components)
- Use hooks for state and side effects
- Keep components focused - one job per component
- Use dedicated page components for each route, with reusable shared components
- Extract reusable logic into custom hooks
