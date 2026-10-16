# TypeScript Types at Runtime

How the frontend turns API responses into typed values, and why `ApiError` is a class. Written during AUTH-01-02.

## Types don't exist at runtime

TypeScript removes every type, interface and type assertion when it compiles. At runtime there is only JavaScript, so:

- TypeScript never checks a value's fields against a type at runtime.
- Nothing turns JSON into a class automatically. Unlike Jackson on the backend, there is no deserialization step.
- `as SomeType` and `value is SomeType` only tell the compiler what to assume. They don't check anything when the code runs.

## How an API response becomes typed

In [`client.ts`](../frontend/src/api/client.ts):

1. `response.json()` returns a plain JavaScript object. TypeScript treats it as `unknown` (or `any`) and checks nothing.
2. `isProblemDetails(body)` is a check we wrote by hand. It only tests that the object has a `status` field. Its return type, `value is ProblemDetails`, tells the compiler to trust the result. What happens at runtime is only the check we wrote.
3. On `if (!response.ok)`, the code calls `new ApiError(response.status, problem)` itself. The object is an `ApiError` only because of that explicit `new`.
4. That's why `error instanceof ApiError` works later, for example in [`RegisterPage.tsx`](../frontend/src/pages/RegisterPage.tsx): the error really was created by the class.

Success bodies work the same way. `(await response.json()) as RegistrationResponse` only makes a promise to the compiler. If the server sent a different shape, nothing would catch it at runtime.

## Why `ApiError` is a class, not a type or interface

- **It can be checked at runtime.** In `catch (error)`, `error` is `unknown`. `instanceof ApiError` tells an API failure apart from anything else, such as a bug or a JSON parse failure. An interface has nothing to check `instanceof` against.
- **It extends `Error`.** It gets a `message`, a stack trace and `name`, and it shows up properly in the console and in tests. Throwing a plain object loses the stack trace.
- **The status and Problem Details travel with it.** It's built from them in one place, and its `message` falls back to a generic one.

The alternative is to throw plain objects and write a check like `isApiError(e): e is ApiError` that looks at their fields. That's more code, has no stack trace, and the check is weaker.

## Checking data at runtime

To actually check a response's shape at runtime, use a schema library such as [Zod](https://zod.dev). It defines a schema once, checks values against it at runtime, and infers the TypeScript type from it. The project doesn't use one yet: API responses are trusted to match the contracts in `context/epics/`.
