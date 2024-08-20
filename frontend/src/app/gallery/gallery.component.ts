import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from "@angular/router";
import {BackendService} from "../backend.service";
import {Channel, ChannelData} from "../channel-data";
import {Location, LocationStrategy} from "@angular/common";
import clientConfiguration from '../../client-configuration.json'
import {PaginatorState} from "primeng/paginator";
import {User} from "../user-data";
import {Message} from "../message-data";
import {MessageService} from "primeng/api";

@Component({
  selector: 'app-stats',
  templateUrl: './gallery.component.html',
  styleUrls: ['./gallery.component.scss'],
  providers: [MessageService]
})
export class GalleryComponent implements OnInit {
  channelData: ChannelData = new ChannelData();
  selectedChannel: Channel;
  dataLoaded: boolean;
  channelNotFound: true;
  tabIndex: number;
  images: any[] | undefined;
  responsiveOptions: any[] | undefined;
  showGallery: boolean = false;
  activeIndex: number;
  first: number = 0;
  rows: number = 100;
  totalRecords: unknown;
  rowsPerPageOptions = [100, 500, 1000];
  selectedDate: Date | undefined;
  selectedUsers: User[] = [];
  users: User[] = [];
  messageFilter: string = "";

  constructor(
    public router: Router,
    private route: ActivatedRoute,
    private backendService: BackendService,
    private location: Location,
    private locationStrategy: LocationStrategy,
    private messageService: MessageService,
  ) { }

  ngOnInit(): void {
    this.responsiveOptions = [
      {
        breakpoint: '1024px',
        numVisible: 5
      },
      {
        breakpoint: '768px',
        numVisible: 3
      },
      {
        breakpoint: '560px',
        numVisible: 1
      }
    ];

    let userIds: string[] = [];

    this.route.pathFromRoot[1].queryParams.subscribe(params => {
      if (Object.prototype.hasOwnProperty.call(params, 'users')) {
        userIds = params['users'].split(",").filter((id: string) => id);
      }
      if (Object.prototype.hasOwnProperty.call(params, 'regex')) {
        this.messageFilter = params['regex'];
      }
      if (Object.prototype.hasOwnProperty.call(params, 'date')) {
        this.selectedDate = new Date(params['date']);
      }
      if (Object.prototype.hasOwnProperty.call(params, 'limit')) {
        const limit = Number(params['limit']);
        if (!isNaN(limit) && this.rowsPerPageOptions.indexOf(limit) !== -1) {
          this.rows = limit;
        }
      }
    });
    this.route.pathFromRoot[1].url.subscribe(val => {
      if (val.length > 2) {
        const page = Number(val[2]);
        this.first = (page - 1) * this.rows;
      }
    });

    this.getUsers(userIds);
  }

  private getUsers(userIds: string[]): void {
    this.backendService.getUsers().subscribe(response => {
      this.users = response.users;
      this.selectedUsers = response.users.filter(user => { return userIds.indexOf(user.id) !== -1 })
      this.getChannels();
    });
  }

  private getChannels(): void {
    this.backendService.getChannels().subscribe(response => {
      this.channelData = response;
      const channel = this.route.snapshot.paramMap.get('channel');
      if (channel == undefined) {
        this.channelNotFound = true;
        this.dataLoaded = true;
      }
      else {
        this.selectChannel(channel);
      }
    });
  }

  private selectChannel(channelId: string): void {
    let channel = this.channelData.channels.find((item) => { return item.id == channelId });
    if (channel == undefined) {
      this.channelNotFound = true;
      this.dataLoaded = true;
      return;
    }
    else {
      this.selectedChannel = channel;
      this.tabIndex = this.channelData.channels.indexOf(channel);
      this.loadData();
    }
  }

  private getSelectedDate(): string {
    if (this.selectedDate == undefined) {
      return ""
    }
    return new Date(this.selectedDate.getTime() - this.selectedDate.getTimezoneOffset()*60000).toISOString().split('T')[0]
  }

  private loadData() {
    this.dataLoaded = false;

    const page = (this.first / this.rows) + 1;
    const userIds = this.selectedUsers.map(user => { return user.id });
    const date = this.getSelectedDate();

    this.backendService.getMessages(this.selectedChannel, page, this.rows, "desc", userIds, this.messageFilter, date, true).subscribe(response => {
      this.images = [];
      this.totalRecords = response.messageCount;
      response.messages.forEach(item => {
        item.attachments.forEach(attachment => {
          let filename = attachment.titleLink;
          while(filename.startsWith("/")) {
            filename = filename.substring(1);
          }
          this.images?.push({
            url: clientConfiguration.rocketchatUrl + filename,
            title: attachment.title,
            description: attachment.description,
            message: item
          })
        });
      });
      this.dataLoaded = true;
      this.updateUrl();
    });
  }
  navigateToArchive(): void {
    if (this.selectedChannel.id == 'all') {
      this.router.navigate(['/']).then();
    }
    else {
      this.router.navigate(['/' + this.selectedChannel.id]).then();
    }
  }

  handleTabChange(event: any) {
    this.selectedChannel = this.channelData.channels[event.index];
    this.loadData();
  }

  private updateUrl(): void {
    const page = (this.first / this.rows) + 1;
    let url = '/gallery/' + this.selectedChannel.id + '/' + page;

    let parameters: string[] = [];
    if (this.selectedUsers.length > 0) {
      parameters.push('users=' + this.selectedUsers.map(user => encodeURIComponent(user.id)).join(','));
    }
    if (this.messageFilter) {
      parameters.push('regex=' + encodeURIComponent(this.messageFilter));
    }
    if (this.selectedDate) {
      parameters.push('date=' + encodeURIComponent(this.getSelectedDate()));
    }
    if (this.rows != this.rowsPerPageOptions[0]) {
      parameters.push('limit=' + this.rows);
    }
    if (parameters.length > 0) {
      url += '?';
      url += parameters.join("&")
    }

    this.location.go(url);
  }

  imageClick(index: number) {
    this.activeIndex = index;
    this.showGallery = true;
  }

  onPageChange(event: PaginatorState) {
    if (event.first != null && event.rows != null) {
      this.first = event.first;
      this.rows = event.rows;

      this.loadData();
    }
  }

  applyFilter() {
    this.first = 0;
    this.loadData();
  }

  clearFilter() {
    this.selectedDate = undefined;
    this.selectedUsers = [];
    this.messageFilter = "";

    this.applyFilter();
  }

  messageClick(message: Message) {
      let url = location.origin + this.locationStrategy.getBaseHref() + "channel/" + encodeURIComponent(this.selectedChannel.id) + "/" + encodeURIComponent(message.id);
      navigator.clipboard.writeText(url).then(() => {
        this.messageService.add({ severity: 'success', summary: 'Link copied to clipboard'});
      }).catch(() => {
        this.messageService.add({ severity: 'error', summary: 'Error copying link to clipboard'});
      });
  }
}
