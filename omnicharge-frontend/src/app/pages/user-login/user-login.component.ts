import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-user-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './user-login.component.html',
  styleUrls: ['./user-login.component.css']
})
export class UserLoginComponent implements OnInit {
  username = '';
  password = '';
  loading = false;
  errorMessage = '';
  successMessage = '';

  constructor(private authService: AuthService, private router: Router) {}

  ngOnInit(): void {
    // Clear any existing session (admin or user) when entering login page
    this.authService.clearAndLogout();
  }

  onLogin(): void {
    this.errorMessage = '';
    this.successMessage = '';

    if (!this.username.trim() || !this.password.trim()) {
      this.errorMessage = 'Username and password are required';
      return;
    }

    this.loading = true;
    this.authService.loginUser({ username: this.username, password: this.password }).subscribe({
      next: (response) => {
        this.loading = false;
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.loading = false;
        if (err.status === 401) {
          const body = err.error;
          this.errorMessage = body?.error || 'Invalid credentials';
        } else if (err.status === 400) {
          const body = err.error;
          if (typeof body === 'object') {
            const messages = Object.values(body).join(', ');
            this.errorMessage = messages || 'Invalid request';
          } else {
            this.errorMessage = body?.error || 'Invalid request';
          }
        } else {
          this.errorMessage = err.error?.error || 'Login failed. Please try again.';
        }
      }
    });
  }
}
