import {Injectable} from "@angular/core";
import {
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpParameterCodec,
  HttpParams,
  HttpRequest
} from "@angular/common/http";
import {Observable} from "rxjs";

// workaround for https://github.com/angular/angular/issues/11058
// solution from https://github.com/angular/angular/issues/11058#issuecomment-717239897
@Injectable()
export class EncodeUrlParamsSafelyInterceptor implements HttpInterceptor, HttpParameterCodec {
  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const newParams = new HttpParams({
      fromString: req.params.toString(),
      encoder: this,
    });

    return next.handle(req.clone({
      params: newParams,
    }));
  }

  encodeKey(key: string): string {
    return encodeURIComponent(key);
  }

  encodeValue(value: string): string {
    return encodeURIComponent(value);
  }

  decodeKey(key: string): string {
    return decodeURIComponent(key);
  }

  decodeValue(value: string): string {
    return decodeURIComponent(value);
  }
}
