import {Component, OnInit} from '@angular/core';
import {BackendService} from "./backend.service";
import gitData from '../git-version.json'
import {RouterOutlet} from "@angular/router";


@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  imports: [
    RouterOutlet
],
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

      console.log("Frontend revision: " + gitData.shortSHA);
      console.log("Backend revision: " + backendVersion);

      this.versionError = gitData.shortSHA != backendVersion;

      if (this.versionError) {
        console.log("Frontend and backend revision do NOT match!");
      }
      else {
        console.log("Frontend and backend revision match");
      }
    });
  }
}
