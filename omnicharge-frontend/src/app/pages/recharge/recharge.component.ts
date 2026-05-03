import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { OperatorService, OperatorDto, RechargePlanDto } from '../../services/operator.service';
import { RechargeService } from '../../services/recharge.service';

@Component({
  selector: 'app-recharge',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './recharge.component.html',
  styleUrl: './recharge.component.css'
})
export class RechargeComponent implements OnInit {
  step = 1;
  mobileNumber = '';
  mobileError = '';
  operators: OperatorDto[] = [];
  selectedOperator: OperatorDto | null = null;
  plans: RechargePlanDto[] = [];
  selectedPlan: RechargePlanDto | null = null;
  loadingOperators = true;
  loadingPlans = false;
  submitting = false;
  errorMessage = '';
  successMessage = '';

  operatorTypeFilter = '';
  planCategoryFilter = '';

  get filteredOperators() {
    if (!this.operatorTypeFilter) return this.operators;
    return this.operators.filter(o => o.type === this.operatorTypeFilter);
  }

  get filteredPlans() {
    if (!this.planCategoryFilter) return this.plans;
    return this.plans.filter(p => p.category === this.planCategoryFilter);
  }

  constructor(
    private operatorService: OperatorService,
    private rechargeService: RechargeService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.operatorService.getAllOperators().subscribe({
      next: (ops) => { this.operators = ops; this.loadingOperators = false; },
      error: () => { this.loadingOperators = false; this.errorMessage = 'Failed to load operators'; }
    });
  }

  selectOperator(op: OperatorDto): void {
    if (op.status !== 'ACTIVE') {
      this.errorMessage = 'Operator ' + op.name + ' is currently inactive';
      return;
    }
    this.errorMessage = '';
    this.selectedOperator = op;
  }

  goToStep2(): void {
    this.mobileError = '';
    this.errorMessage = '';
    if (!this.mobileNumber || !/^[6-9]\d{9}$/.test(this.mobileNumber)) {
      this.mobileError = 'Invalid mobile number. Must be a valid 10 digit Indian mobile number';
      return;
    }
    if (!this.selectedOperator) {
      this.errorMessage = 'Please select an operator';
      return;
    }
    this.step = 2;
    this.loadingPlans = true;
    this.operatorService.getActivePlansByOperator(this.selectedOperator.id).subscribe({
      next: (plans) => { this.plans = plans; this.loadingPlans = false; },
      error: () => { this.loadingPlans = false; this.errorMessage = 'Failed to load plans'; }
    });
  }

  selectPlan(plan: RechargePlanDto): void {
    if (plan.status !== 'ACTIVE') {
      this.errorMessage = 'Plan ' + plan.planName + ' is currently inactive';
      return;
    }
    this.errorMessage = '';
    this.selectedPlan = plan;
  }

  goToStep3(): void {
    if (!this.selectedPlan) { this.errorMessage = 'Please select a plan'; return; }
    this.errorMessage = '';
    this.step = 3;
  }

  initiateRecharge(): void {
    if (!this.selectedOperator || !this.selectedPlan) return;
    this.errorMessage = '';
    this.submitting = true;
    this.rechargeService.initiateRecharge({
      mobileNumber: this.mobileNumber,
      operatorId: this.selectedOperator.id,
      planId: this.selectedPlan.id
    }).subscribe({
      next: (recharge) => { this.submitting = false; this.router.navigate(['/payment', recharge.id]); },
      error: (err) => {
        this.submitting = false;
        this.errorMessage = err.error?.error || err.error?.message || 'Failed to initiate recharge';
      }
    });
  }
}
