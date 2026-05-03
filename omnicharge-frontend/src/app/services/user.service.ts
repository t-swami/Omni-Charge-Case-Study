import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface UserDto {
  id: number;
  username: string;
  email: string;
  fullName: string;
  phone: string;
  role: string;
  active: boolean;
  walletBalance: number;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

@Injectable({ providedIn: 'root' })
export class UserService {
  private apiUrl = '/api/users';

  constructor(private http: HttpClient) {}

  getProfile(): Observable<UserDto> {
    return this.http.get<UserDto>(`${this.apiUrl}/profile`);
  }

  getAllUsers(): Observable<UserDto[]> {
    return this.http.get<UserDto[]>(`${this.apiUrl}/all`);
  }

  promoteToAdmin(userId: number): Observable<UserDto> {
    return this.http.put<UserDto>(`${this.apiUrl}/promote/${userId}`, {});
  }

  changePassword(request: ChangePasswordRequest): Observable<{ message: string }> {
    return this.http.put<{ message: string }>(`${this.apiUrl}/change-password`, request);
  }

  getWalletBalance(): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/profile/wallet`);
  }
}
