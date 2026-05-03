import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface OperatorDto {
  id: number;
  name: string;
  type: string;
  status: string;
  logoUrl: string;
  description: string;
}

export interface OperatorRequest {
  name: string;
  type: string;
  status: string;
  logoUrl: string;
  description: string;
}

export interface RechargePlanDto {
  id: number;
  planName: string;
  price: number;
  validity: string;
  data: string;
  calls: string;
  sms: string;
  description: string;
  category: string;
  status: string;
  operatorId: number;
  operatorName: string;
}

export interface RechargePlanRequest {
  planName: string;
  price: number;
  validity: string;
  data: string;
  calls: string;
  sms: string;
  description: string;
  category: string;
  status: string;
  operatorId: number;
}

@Injectable({ providedIn: 'root' })
export class OperatorService {
  private operatorUrl = '/api/operators';
  private planUrl = '/api/plans';

  constructor(private http: HttpClient) {}

  // Operator endpoints
  getAllOperators(): Observable<OperatorDto[]> {
    return this.http.get<OperatorDto[]>(this.operatorUrl);
  }

  getOperatorById(id: number): Observable<OperatorDto> {
    return this.http.get<OperatorDto>(`${this.operatorUrl}/${id}`);
  }

  getOperatorsByStatus(status: string): Observable<OperatorDto[]> {
    return this.http.get<OperatorDto[]>(`${this.operatorUrl}/status/${status}`);
  }

  getOperatorsByType(type: string): Observable<OperatorDto[]> {
    return this.http.get<OperatorDto[]>(`${this.operatorUrl}/type/${type}`);
  }

  addOperator(request: OperatorRequest): Observable<OperatorDto> {
    return this.http.post<OperatorDto>(this.operatorUrl, request);
  }

  updateOperator(id: number, request: OperatorRequest): Observable<OperatorDto> {
    return this.http.put<OperatorDto>(`${this.operatorUrl}/${id}`, request);
  }

  deleteOperator(id: number): Observable<string> {
    return this.http.delete(`${this.operatorUrl}/${id}`, { responseType: 'text' });
  }

  // Plan endpoints
  getAllPlans(): Observable<RechargePlanDto[]> {
    return this.http.get<RechargePlanDto[]>(this.planUrl);
  }

  getPlanById(id: number): Observable<RechargePlanDto> {
    return this.http.get<RechargePlanDto>(`${this.planUrl}/${id}`);
  }

  getPlansByOperator(operatorId: number): Observable<RechargePlanDto[]> {
    return this.http.get<RechargePlanDto[]>(`${this.planUrl}/operator/${operatorId}`);
  }

  getActivePlansByOperator(operatorId: number): Observable<RechargePlanDto[]> {
    return this.http.get<RechargePlanDto[]>(`${this.planUrl}/operator/${operatorId}/active`);
  }

  getPlansByCategory(category: string): Observable<RechargePlanDto[]> {
    return this.http.get<RechargePlanDto[]>(`${this.planUrl}/category/${category}`);
  }

  addPlan(request: RechargePlanRequest): Observable<RechargePlanDto> {
    return this.http.post<RechargePlanDto>(this.planUrl, request);
  }

  updatePlan(id: number, request: RechargePlanRequest): Observable<RechargePlanDto> {
    return this.http.put<RechargePlanDto>(`${this.planUrl}/${id}`, request);
  }

  deletePlan(id: number): Observable<string> {
    return this.http.delete(`${this.planUrl}/${id}`, { responseType: 'text' });
  }
}
