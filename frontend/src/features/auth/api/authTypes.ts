export interface RegisterRequest {
  email: string
  password: string
  displayName: string
}

export interface RegisteredUser {
  id: string
  email: string
  displayName: string
  createdAt: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface CurrentUser {
  id: string
  email: string
  displayName: string
}
