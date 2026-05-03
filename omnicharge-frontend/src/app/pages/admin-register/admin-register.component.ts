import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-admin-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './admin-register.component.html',
  styleUrls: ['./admin-register.component.css']
})
export class AdminRegisterComponent {
  form = { username: '', email: '', password: '', fullName: '', phone: '', adminSecretKey: '' };
  loading = false;
  errorMessage = '';
  successMessage = '';
  fieldErrors: { [key: string]: string } = {};

  constructor(private authService: AuthService, private router: Router) {}

  onRegister(): void {
    this.errorMessage = '';
    this.successMessage = '';
    this.fieldErrors = {};

    // Client-side validations matching backend
    if (!this.form.username || this.form.username.length < 3 || this.form.username.length > 30) {
      this.fieldErrors['username'] = 'Username must be between 3 and 30 characters';
      return;
    }
    if (!/^[a-zA-Z0-9_]+$/.test(this.form.username)) {
      this.fieldErrors['username'] = 'Username can only contain letters, numbers, and underscores';
      return;
    }
    if (!this.form.email || !/^[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}$/.test(this.form.email)) {
      this.fieldErrors['email'] = 'Please provide a valid email address';
      return;
    }
    if (!this.form.fullName || this.form.fullName.length < 2 || this.form.fullName.length > 100) {
      this.fieldErrors['fullName'] = 'Full name must be between 2 and 100 characters';
      return;
    }
    if (!this.form.phone || !/^[6-9]\d{9}$/.test(this.form.phone)) {
      this.fieldErrors['phone'] = 'Please provide a valid 10-digit Indian mobile number';
      return;
    }
    if (!this.form.password || this.form.password.length < 6) {
      this.fieldErrors['password'] = 'Password must be at least 6 characters';
      return;
    }
    if (!this.form.adminSecretKey.trim()) {
      this.fieldErrors['adminSecretKey'] = 'Admin secret key is required';
      return;
    }

    this.loading = true;
    this.authService.registerAdmin(this.form).subscribe({
      next: () => {
        this.loading = false;
        this.successMessage = 'Admin account created successfully! Redirecting to admin login...';
        setTimeout(() => this.router.navigate(['/admin/login']), 1500);
      },
      error: (err) => {
        this.loading = false;
        if (err.status === 401) {
          this.errorMessage = err.error?.error || 'Invalid admin secret key';
        } else if (err.status === 400 && typeof err.error === 'object' && !err.error.error) {
          this.fieldErrors = err.error;
        } else if (err.status === 409) {
          this.errorMessage = err.error?.error || 'Username or email already exists';
        } else {
          this.errorMessage = err.error?.error || 'Registration failed. Please try again.';
        }
      }
    });
  }
}
