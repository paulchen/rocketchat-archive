import {Component, OnInit} from '@angular/core';
import {BackendService} from "./backend.service";
import gitData from '../git-version.json'

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {
  versionError: boolean;

  constructor(private backendService: BackendService) { }

  ngOnInit(): void {
    this.checkVersion();
  }

  private checkVersion(): void {
    this.backendService.getVersion().subscribe(response => {
      const backendVersion = response.version;
      this.versionError = gitData.shortSHA != backendVersion;
    });
  }
}
