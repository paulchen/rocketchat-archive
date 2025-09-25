import {Injectable} from "@angular/core";
import {BackendService} from "./backend.service";
import {firstValueFrom, pipe, tap} from "rxjs";

@Injectable({
  providedIn: "root"
})
export class ConfigService {
  private config: Config | undefined = undefined;

  constructor(private backend: BackendService) {}

  loadConfig() {
    return firstValueFrom(this.backend.getConfig().pipe(tap(config => this.config = config)));
  }

  getConfig(): Config {
    return this.config!!
  }
}

export class Config {
  rocketchatUrl: string;
}
