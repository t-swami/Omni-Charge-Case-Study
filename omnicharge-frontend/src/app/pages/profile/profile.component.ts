import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserService, UserDto, ChangePasswordRequest } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profile.component.html'
})
export class ProfileComponent implements OnInit {
  profile: UserDto | null = null;
  loading = true;
  errorMessage = '';
  successMessage = '';

  // Change password
  showChangePassword = false;
  pwForm: ChangePasswordRequest = { currentPassword: '', newPassword: '', confirmPassword: '' };
  pwError = '';
  pwSuccess = '';
  pwLoading = false;

  constructor(private userService: UserService, public authService: AuthService) {}

  ngOnInit(): void {
    this.userService.getProfile().subscribe({
      next: (p) => { this.profile = p; this.loading = false; },
      error: (err) => { this.loading = false; this.errorMessage = err.error?.error || 'Failed to load profile'; }
    });
  }

  changePassword(): void {
    this.pwError = '';
    this.pwSuccess = '';

    // Client-side validation matching backend
    if (!this.pwForm.currentPassword || !this.pwForm.newPassword || !this.pwForm.confirmPassword) {
      this.pwError = 'All password fields are required';
      return;
    }
    if (this.pwForm.newPassword !== this.pwForm.confirmPassword) {
      this.pwError = 'New password and confirm password do not match';
      return;
    }
    if (this.pwForm.newPassword.length < 6) {
      this.pwError = 'New password must be at least 6 characters';
      return;
    }
    if (this.pwForm.currentPassword === this.pwForm.newPassword) {
      this.pwError = 'New password must be different from current password';
      return;
    }

    this.pwLoading = true;
    this.userService.changePassword(this.pwForm).subscribe({
      next: (res) => {
        this.pwLoading = false;
        this.pwSuccess = res.message || 'Password changed successfully';
        this.pwForm = { currentPassword: '', newPassword: '', confirmPassword: '' };
      },
      error: (err) => {
        this.pwLoading = false;
        this.pwError = err.error?.error || 'Failed to change password';
      }
    });
  }
}
