import {Component, OnInit} from '@angular/core';
import {BackendService} from "./backend.service";

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {
  response = "loading"

  constructor(private backendService: BackendService) { }

  ngOnInit(): void {
    this.backendService.getTest().subscribe(response => {
      this.response = response.response
    });
  }


}
