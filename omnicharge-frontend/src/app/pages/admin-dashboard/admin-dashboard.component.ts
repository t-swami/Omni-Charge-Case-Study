import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserService, UserDto } from '../../services/user.service';
import { OperatorService, OperatorDto, OperatorRequest, RechargePlanDto, RechargePlanRequest } from '../../services/operator.service';
import { RechargeService, RechargeRequestDto } from '../../services/recharge.service';
import { PaymentService, TransactionDto } from '../../services/payment.service';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.css'
})
export class AdminDashboardComponent implements OnInit {
  activeTab = 'overview';

  // Data
  users: UserDto[] = [];
  operators: OperatorDto[] = [];
  plans: RechargePlanDto[] = [];
  recharges: RechargeRequestDto[] = [];
  transactions: TransactionDto[] = [];

  // Loading
  loadingUsers = true;
  loadingOperators = true;
  loadingPlans = true;
  loadingRecharges = true;
  loadingTransactions = true;

  // Filters
  operatorStatusFilter = '';
  operatorTypeFilter = '';
  
  planOperatorFilter = 0;
  planCategoryFilter = '';

  rechargeStatusFilter = '';
  rechargeMobileFilter = '';

  transactionStatusFilter = '';
  transactionMobileFilter = '';

  // Messages
  errorMessage = '';
  successMessage = '';

  // Operator form
  showOperatorForm = false;
  editingOperatorId: number | null = null;
  operatorForm: OperatorRequest = { name: '', type: '', status: 'ACTIVE', logoUrl: '', description: '' };

  // Plan form
  showPlanForm = false;
  editingPlanId: number | null = null;
  planForm: RechargePlanRequest = { planName: '', price: 0, validity: '', data: '', calls: '', sms: '', description: '', category: '', status: 'ACTIVE', operatorId: 0 };

  constructor(
    private userService: UserService,
    private operatorService: OperatorService,
    private rechargeService: RechargeService,
    private paymentService: PaymentService
  ) {}

  ngOnInit(): void {
    this.loadAll();
  }

  loadAll(): void {
    this.userService.getAllUsers().subscribe({
      next: (u) => { this.users = u; this.loadingUsers = false; },
      error: () => this.loadingUsers = false
    });
    this.operatorService.getAllOperators().subscribe({
      next: (o) => { this.operators = o; this.loadingOperators = false; },
      error: () => this.loadingOperators = false
    });
    this.operatorService.getAllPlans().subscribe({
      next: (p) => { this.plans = p; this.loadingPlans = false; },
      error: () => this.loadingPlans = false
    });
    this.rechargeService.getAllRecharges().subscribe({
      next: (r) => { this.recharges = r; this.loadingRecharges = false; },
      error: () => this.loadingRecharges = false
    });
    this.paymentService.getAllTransactions().subscribe({
      next: (t) => { this.transactions = t; this.loadingTransactions = false; },
      error: () => this.loadingTransactions = false
    });
  }

  // User management
  promoteUser(userId: number): void {
    if (!confirm('Promote this user to admin?')) return;
    this.userService.promoteToAdmin(userId).subscribe({
      next: () => {
        this.successMessage = 'User promoted to admin';
        this.userService.getAllUsers().subscribe({ next: (u) => this.users = u });
      },
      error: (err) => this.errorMessage = err.error?.error || 'Failed to promote user'
    });
  }

  // Operator CRUD
  openOperatorForm(op?: OperatorDto): void {
    this.showOperatorForm = true;
    this.showPlanForm = false;
    if (op) {
      this.editingOperatorId = op.id;
      this.operatorForm = { name: op.name, type: op.type, status: op.status, logoUrl: op.logoUrl || '', description: op.description || '' };
    } else {
      this.editingOperatorId = null;
      this.operatorForm = { name: '', type: '', status: 'ACTIVE', logoUrl: '', description: '' };
    }
  }

  saveOperator(): void {
    this.errorMessage = '';
    if (!this.operatorForm.name || !this.operatorForm.type) {
      this.errorMessage = 'Operator name and type are required';
      return;
    }
    const obs = this.editingOperatorId
      ? this.operatorService.updateOperator(this.editingOperatorId, this.operatorForm)
      : this.operatorService.addOperator(this.operatorForm);

    obs.subscribe({
      next: () => {
        this.successMessage = this.editingOperatorId ? 'Operator updated' : 'Operator created';
        this.showOperatorForm = false;
        this.operatorService.getAllOperators().subscribe({ next: (o) => this.operators = o });
      },
      error: (err) => this.errorMessage = err.error?.error || 'Failed to save operator'
    });
  }

  deleteOperator(id: number): void {
    if (!confirm('Delete this operator?')) return;
    this.operatorService.deleteOperator(id).subscribe({
      next: () => {
        this.successMessage = 'Operator deleted';
        this.operatorService.getAllOperators().subscribe({ next: (o) => this.operators = o });
      },
      error: (err) => this.errorMessage = err.error?.error || err.error || 'Failed to delete operator'
    });
  }

  // Plan CRUD
  openPlanForm(plan?: RechargePlanDto): void {
    this.showPlanForm = true;
    this.showOperatorForm = false;
    if (plan) {
      this.editingPlanId = plan.id;
      this.planForm = {
        planName: plan.planName, price: plan.price, validity: plan.validity,
        data: plan.data, calls: plan.calls, sms: plan.sms,
        description: plan.description || '', category: plan.category,
        status: plan.status, operatorId: plan.operatorId
      };
    } else {
      this.editingPlanId = null;
      this.planForm = { planName: '', price: 0, validity: '', data: '', calls: '', sms: '', description: '', category: '', status: 'ACTIVE', operatorId: 0 };
    }
  }

  savePlan(): void {
    this.errorMessage = '';
    if (!this.planForm.planName || !this.planForm.price || !this.planForm.operatorId || !this.planForm.category) {
      this.errorMessage = 'Plan name, price, operator, and category are required';
      return;
    }
    const obs = this.editingPlanId
      ? this.operatorService.updatePlan(this.editingPlanId, this.planForm)
      : this.operatorService.addPlan(this.planForm);

    obs.subscribe({
      next: () => {
        this.successMessage = this.editingPlanId ? 'Plan updated' : 'Plan created';
        this.showPlanForm = false;
        this.operatorService.getAllPlans().subscribe({ next: (p) => this.plans = p });
      },
      error: (err) => this.errorMessage = err.error?.error || 'Failed to save plan'
    });
  }

  deletePlan(id: number): void {
    if (!confirm('Delete this plan?')) return;
    this.operatorService.deletePlan(id).subscribe({
      next: () => {
        this.successMessage = 'Plan deleted';
        this.operatorService.getAllPlans().subscribe({ next: (p) => this.plans = p });
      },
      error: (err) => this.errorMessage = err.error?.error || err.error || 'Failed to delete plan'
    });
  }

  // Cancel recharge
  cancelRecharge(id: number): void {
    if (!confirm('Cancel this recharge?')) return;
    this.rechargeService.cancelRecharge(id).subscribe({
      next: () => {
        this.successMessage = 'Recharge cancelled';
        this.rechargeService.getAllRecharges().subscribe({ next: (r) => this.recharges = r });
      },
      error: (err) => this.errorMessage = err.error?.error || err.error?.message || 'Failed to cancel'
    });
  }

  countByStatus(status: string): number {
    return this.transactions.filter(t => t.status === status).length;
  }

  clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }

  // Filter methods
  applyOperatorFilter() {
    this.loadingOperators = true;
    if (this.operatorStatusFilter) {
      this.operatorService.getOperatorsByStatus(this.operatorStatusFilter).subscribe({next: o => { this.operators = o; this.loadingOperators = false; }, error: () => this.loadingOperators = false});
    } else if (this.operatorTypeFilter) {
      this.operatorService.getOperatorsByType(this.operatorTypeFilter).subscribe({next: o => { this.operators = o; this.loadingOperators = false; }, error: () => this.loadingOperators = false});
    } else {
      this.operatorService.getAllOperators().subscribe({next: o => { this.operators = o; this.loadingOperators = false; }, error: () => this.loadingOperators = false});
    }
  }

  applyPlanFilter() {
    this.loadingPlans = true;
    if (this.planOperatorFilter) {
      this.operatorService.getPlansByOperator(this.planOperatorFilter).subscribe({next: p => { this.plans = p; this.loadingPlans = false; }, error: () => this.loadingPlans = false});
    } else if (this.planCategoryFilter) {
      this.operatorService.getPlansByCategory(this.planCategoryFilter).subscribe({next: p => { this.plans = p; this.loadingPlans = false; }, error: () => this.loadingPlans = false});
    } else {
      this.operatorService.getAllPlans().subscribe({next: p => { this.plans = p; this.loadingPlans = false; }, error: () => this.loadingPlans = false});
    }
  }

  applyRechargeFilter() {
    this.loadingRecharges = true;
    if (this.rechargeStatusFilter) {
      this.rechargeService.getRechargesByStatus(this.rechargeStatusFilter).subscribe({next: r => { this.recharges = r; this.loadingRecharges = false; }, error: () => this.loadingRecharges = false});
    } else if (this.rechargeMobileFilter) {
      this.rechargeService.getRechargesByMobile(this.rechargeMobileFilter).subscribe({next: r => { this.recharges = r; this.loadingRecharges = false; }, error: () => this.loadingRecharges = false});
    } else {
      this.rechargeService.getAllRecharges().subscribe({next: r => { this.recharges = r; this.loadingRecharges = false; }, error: () => this.loadingRecharges = false});
    }
  }

  applyTransactionFilter() {
    this.loadingTransactions = true;
    if (this.transactionStatusFilter) {
      this.paymentService.getTransactionsByStatus(this.transactionStatusFilter).subscribe({next: t => { this.transactions = t; this.loadingTransactions = false; }, error: () => this.loadingTransactions = false});
    } else if (this.transactionMobileFilter) {
      this.paymentService.getTransactionsByMobile(this.transactionMobileFilter).subscribe({next: t => { this.transactions = t; this.loadingTransactions = false; }, error: () => this.loadingTransactions = false});
    } else {
      this.paymentService.getAllTransactions().subscribe({next: t => { this.transactions = t; this.loadingTransactions = false; }, error: () => this.loadingTransactions = false});
    }
  }
}
