import {Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {TestResponse} from "./test-response";

@Injectable({
  providedIn: 'root'
})
export class BackendService {
  constructor(private http: HttpClient) { }

  getTest(): Observable<TestResponse> {
    return this.http.get<TestResponse>("/services/test");
  }
}


