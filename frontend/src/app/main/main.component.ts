import {ChangeDetectorRef, Component, OnInit, ViewChild} from '@angular/core';
import {Channel, ChannelData} from "../channel-data";
import {Attachment, Message, MessageData} from "../message-data";
import {User} from "../user-data";
import {BackendService} from "../backend.service";
import {ActivatedRoute, Router} from "@angular/router";
import {
  DatePipe,
  Location,
  LocationStrategy,
  NgForOf,
  NgIf,
  NgStyle,
  ViewportScroller
} from "@angular/common";
import {FilterMatchMode, MenuItem, MessageService} from "primeng/api";
import clientConfiguration from '../../client-configuration.json'
import {Table, TableModule} from "primeng/table";
import {sortChannels} from "../util";
import {Dialog} from "primeng/dialog";
import {ContextMenu} from "primeng/contextmenu";
import {TabPanel, TabView} from "primeng/tabview";
import {ButtonDirective, ButtonLabel} from "primeng/button";
import {Calendar} from "primeng/calendar";
import {MultiSelect} from "primeng/multiselect";
import {FormsModule} from "@angular/forms";
import {Toast} from "primeng/toast";

@Component({
  selector: 'app-main',
  templateUrl: './main.component.html',
  styleUrls: ['./main.component.scss'],
  imports: [
    Dialog,
    TableModule,
    DatePipe,
    ContextMenu,
    NgIf,
    TabView,
    TabPanel,
    NgForOf,
    ButtonDirective,
    ButtonLabel,
    Calendar,
    MultiSelect,
    FormsModule,
    NgStyle,
    Toast
  ],
  providers: [MessageService]
})
export class MainComponent implements OnInit {
  channelData: ChannelData = { channels: [], favouriteChannels: []};
  selectedChannel: Channel;
  messageData: MessageData = { messages: [], messageCount: 0 };
  users: User[] = [];
  limit = 100;
  loading = true;
  private timeout: number;
  tabIndex: number;
  first: number = 0;
  private initialFirst: number | null = null;
  private reloadCount: number = 0;
  private filterInUrl: boolean = false;
  highlightedMessage: string | null = null;
  contextMenuItems: MenuItem[];
  selectedMessage: Message;
  channelNotFound: boolean = true;
  messageNotFound: boolean = false;
  private userIdFilter: string[];
  private messageFilter: string;
  private dateFilter: string;
  rocketchatUrl: string;
  showImageOverlay: boolean = false;
  showAudioOverlay: boolean = false;
  showVideoOverlay: boolean = false;
  overlayTitle: string;
  overlayFile: string;
  showHistoryOverlay: boolean = false;
  messageHistory: Message[] = []
  rowsPerPageOptions = [100, 500, 1000];
  matchModeOptions = [{ label: 'Filter', value: FilterMatchMode.EQUALS }];

  @ViewChild("table") table: Table;

  constructor(
    private backendService: BackendService,
    private route: ActivatedRoute,
    private location: Location,
    private locationStrategy: LocationStrategy,
    private messageService: MessageService,
    private viewportScroller: ViewportScroller,
    private router: Router,
    private changeDetectorRef: ChangeDetectorRef
  ) { }

  ngOnInit(): void {
    this.rocketchatUrl = clientConfiguration.rocketchatUrl;

    this.contextMenuItems = [
      { label: 'Copy link to archive', command: () => this.createLink(false, this.selectedMessage) },
      { label: 'Copy link to Rocket.Chat', command: () => this.createLink(true, this.selectedMessage) },
      { label: 'Navigate to message', command: () => this.navigateToMessage(this.selectedMessage) },
    ];

    let page = null;
    let channel = null;
    let message = null;
    let userIds: string[] = [];
    let regex = null;
    let date = null;
    this.route.pathFromRoot[1].queryParams.subscribe(params => {
      if (Object.prototype.hasOwnProperty.call(params, 'users')) {
        this.filterInUrl = true;
        userIds = params['users'].split(",").filter((id: string) => id);
      }
      if (Object.prototype.hasOwnProperty.call(params, 'regex')) {
        this.filterInUrl = true;
        regex = params['regex'];
      }
      if (Object.prototype.hasOwnProperty.call(params, 'date')) {
        this.filterInUrl = true;
        date = new Date(params['date']);
      }
      if (Object.prototype.hasOwnProperty.call(params, 'limit')) {
        const limit = Number(params['limit']);
        if (!isNaN(limit) && this.rowsPerPageOptions.indexOf(limit) !== -1) {
          this.limit = limit;
        }
      }
    });
    this.route.pathFromRoot[1].url.subscribe(val => {
      if (val.length > 0) {
        channel = val[0];
      }
      if (val.length > 1) {
        if (isNaN(Number(val[1]))) {
          message = val[1];
          this.highlightedMessage = message.path;
        }
        else {
          page = Number(val[1]);
          this.first = (page - 1) * this.limit;
        }
      }
      if (val.length > 2) {
        this.highlightedMessage = val[2].path;
      }
    });

    if (channel && message && !page) {
      this.getPageForMessage(channel, message);
    }
    else {
      this.initialFirst = this.first
      this.getChannels(channel, regex, userIds, date);
    }
  }

  private getPageForMessage(channel: string, message: string) {
    this.backendService.getMessage(channel, message).subscribe({
      next: response => {
        this.first = (response.page - 1) * this.limit;
        this.getChannels(response.channel);
      },
      error: () => {
        this.messageNotFound = true;
        this.channelNotFound = false;
        this.loading = false;
      }
    });
  }

  private createLink(rocketchat: boolean, selectedMessage: Message) {
    const channel = this.getChannel(selectedMessage.rid);
    if (channel == null) {
      this.messageService.add({ severity: 'error', summary: 'Error creating link'});
      return;
    }
    let url;
    if(rocketchat) {
      url = this.rocketchatUrl + "channel/" + encodeURIComponent(channel.name.substring(1)) + "?msg=" + encodeURIComponent(selectedMessage.id);
    }
    else {
      url = location.origin + this.locationStrategy.getBaseHref() + encodeURIComponent(channel.id) + "/" + encodeURIComponent(selectedMessage.id);
    }
    navigator.clipboard.writeText(url).then(() => {
      this.messageService.add({ severity: 'success', summary: 'Link copied to clipboard'});
    }).catch(() => {
      this.messageService.add({ severity: 'error', summary: 'Error copying link to clipboard'});
    });
  }

  private navigateToMessage(selectedMessage: Message) {
    clearTimeout(this.timeout);
    this.router.navigateByUrl('/' + encodeURIComponent(selectedMessage.rid) + "/" + encodeURIComponent(selectedMessage.id)).then(() => {
      this.ngOnInit();
    });
  }

  private getChannels(channel: String | null, regex: String | null = null, userIds: string[] = [], date: Date | null = null): void {
    this.backendService.getChannels().subscribe(response => {
      this.channelData = sortChannels(response);
      this.channelData.channels.forEach(channel => channel.name = '#' + channel.name)
      this.channelData.channels.unshift({ name: 'all', id: 'all'});

      this.selectedChannel = this.channelData.channels[1];
      if (channel) {
        this.channelNotFound = true;
        for (let i = 0; i < this.channelData.channels.length; i++) {
          if (this.channelData.channels[i].id == channel) {
            this.selectedChannel = this.channelData.channels[i];
            this.tabIndex = i;
            this.channelNotFound = false;
            break;
          }
        }
      }
      else if (this.channelData.channels.length > 1) {
        this.selectedChannel = this.channelData.channels[1];
        this.tabIndex = 1;
        this.channelNotFound = false;
      }

      if(this.channelNotFound) {
        this.loading = false;
        return;
      }

      this.messageData = new MessageData();

      this.changeDetectorRef.detectChanges();
      if (regex) {
        this.table.filter(regex, 'message', 'equals');
      }
      if (date) {
        this.table.filter(date, 'timestamp', 'equals')
      }
      this.getUsers(userIds);
    });
  }

  private getUsers(userIds: string[]): void {
    this.backendService.getUsers().subscribe(response => {
      this.users = response.users;

      if (userIds.length > 0) {
        this.table.filter(userIds, 'username', 'equals');
      }
    });
  }

  handleTabChange(event: any) {
    this.selectedChannel = this.channelData.channels[event.index];
    this.first = 0;
  }

  handleTableChange(event: any, reload: boolean) {
    let component = this;
    clearTimeout(this.timeout);
    this.timeout = setTimeout(function() { component.reloadData(event, reload) }, 100);
  }

  reloadData(event: any, reload: boolean) {
    if (this.channelNotFound) {
      return;
    }
    if (!reload) {
      this.loading = true;
    }

    this.reloadCount++;

    const limit = event.rows;
    const first = (this.initialFirst != null) ? this.initialFirst : this.first
    // this is a workaround to get around the problem of p-table
    // resetting the pagination when applying a filter
    if (this.initialFirst != null && ((this.filterInUrl && this.reloadCount == 2) || (!this.filterInUrl && this.reloadCount == 1))) {
      this.initialFirst = null;
      this.first = first;
    }
    const page = (first / limit) + 1;

    const sort = (event.sortOrder == -1) ? "desc" : "asc";

    const filters = event.filters
    let userIds = [];
    let message = "";
    let date: string = "";
    if (filters) {
      if ("username" in filters && "value" in filters["username"] && filters["username"]["value"]) {
        userIds = filters["username"]["value"]
      }
      if ("message" in filters && "value" in filters["message"] && filters["message"]["value"]) {
        message = filters["message"]["value"]
      }
      if ("timestamp" in filters && "value" in filters["timestamp"] && filters["timestamp"]["value"]) {
        let timestamp = filters["timestamp"]["value"]
        date = new Date(timestamp.getTime() - timestamp.getTimezoneOffset()*60000).toISOString().split('T')[0]
      }
    }

    this.userIdFilter = userIds;
    this.messageFilter = message;
    this.dateFilter = date;

    const component = this;
    let currentReloadCount = this.reloadCount;
    this.backendService.getMessages(this.selectedChannel, page, limit, sort, userIds, message, date, "none").subscribe({
      next: response => {
        if (currentReloadCount != this.reloadCount) {
          // during the backend, reloadData() was called another time (for pagination, filtering etc.)
          return;
        }
        this.messageData = this.filterAttachments(response);
        this.loading = false;

        if (response.messages.filter(m => m.id == this.highlightedMessage).length == 0) {
          this.highlightedMessage = null;
        }
        this.updateUrl();

        if (this.highlightedMessage && !reload) {
          setTimeout(function() { component.scrollToMessage() }, 100);
        }

        clearTimeout(this.timeout);
        this.timeout = setTimeout(function() { component.handleTableChange(event, true) }, 5000);
      },
      error: () => {
        this.loading = false;

        clearTimeout(this.timeout);
        this.timeout = setTimeout(function() { component.handleTableChange(event, true) }, 5000);
      }
    })
  }

  private filterAttachments(messageData: MessageData): MessageData {
    messageData.messages.forEach(message => {
      message.attachments = message.attachments.filter(attachment => attachment.type != "message");
    });
    return messageData;
  }

  private scrollToMessage(): void {
    if (this.highlightedMessage) {
      let previousMessage;
      for (let message of this.messageData.messages) {
        if (message.id == this.highlightedMessage) {
          break;
        }
        previousMessage = message;
      }

      if (previousMessage) {
        this.viewportScroller.scrollToAnchor(previousMessage.id);
      }
    }
  }

  private updateUrl(): void {
    const page = (this.first / this.limit) + 1;
    let url = '/' + this.selectedChannel.id + '/' + page;

    if (this.highlightedMessage) {
      url += '/' + this.highlightedMessage;
    }

    let parameters: string[] = [];
    if (this.userIdFilter.length > 0) {
      parameters.push('users=' + this.userIdFilter.map(id => encodeURIComponent(id)).join(','));
    }
    if (this.messageFilter) {
      parameters.push('regex=' + encodeURIComponent(this.messageFilter));
    }
    if (this.dateFilter) {
      parameters.push('date=' + encodeURIComponent(this.dateFilter));
    }
    if (this.limit != this.rowsPerPageOptions[0]) {
      parameters.push('limit=' + this.limit);
    }
    if (parameters.length > 0) {
      url += '?';
      url += parameters.join("&")
    }

    this.location.go(url);
  }

  navigateToStats() {
    clearTimeout(this.timeout);
    this.router.navigate(['/stats', this.selectedChannel.id ]).then();
  }

  navigateToReports() {
    clearTimeout(this.timeout);
    this.router.navigate(['/reports']).then();
  }

  navigateToGallery() {
    clearTimeout(this.timeout);
    this.router.navigate(['/gallery', this.selectedChannel.id ]).then();
  }

  getUserId(username: string): string {
    let userId = '';
    this.users.forEach(user => {
      if(user.username == username) {
        userId = user.id
      }
    });
    return userId;
  }

  getChannel(id: string): Channel | null {
    let channel = null;
    this.channelData.channels.forEach(item => {
      if(item.id == id) {
        channel = item;
      }
    });
    return channel;
  }

  showOverlay(attachment: Attachment) {
    let url = this.rocketchatUrl + attachment.titleLink;
    if (attachment.type == 'file') {
      window.open(url, '_blank');
      return;
    }

    this.overlayFile = url;
    if (attachment.description) {
      this.overlayTitle = attachment.description;
    }
    else {
      this.overlayTitle = attachment.title;
    }

    if (attachment.type == 'image') {
      this.showImageOverlay = true;
    }
    else if (attachment.type == 'audio') {
      this.showAudioOverlay = true;
    }
    else if (attachment.type == 'video') {
      this.showVideoOverlay = true;
    }
  }

  showHistory(message: Message) {
    this.backendService.getMessageHistory(message.rid, message.id).subscribe({
        next: response => {
          this.messageHistory = response.history;
          this.showHistoryOverlay = true;
        }
    });
  }

  filterByUsername(username: string) {
    // temporary fix until https://github.com/primefaces/primeng/issues/14904 has been dealt with
    let userId = this.getUserId(username);
    this.table.filters["username"] = { value: [userId], matchMode: 'equals' };
    this.table.filterTimeout = setTimeout(() => {
      this.table._filter();
      this.table.filterTimeout = null;
    }, this.table.filterDelay);
  }
}
