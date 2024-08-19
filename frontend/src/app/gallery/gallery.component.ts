import { Component, OnInit } from '@angular/core';
import {ActivatedRoute, Router} from "@angular/router";
import {BackendService} from "../backend.service";
import {Channel, ChannelData} from "../channel-data";
import {Location} from "@angular/common";
import clientConfiguration from '../../client-configuration.json'
import {PaginatorState} from "primeng/paginator";

@Component({
  selector: 'app-stats',
  templateUrl: './gallery.component.html',
  styleUrls: ['./gallery.component.scss']
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

  constructor(
    public router: Router,
    private route: ActivatedRoute,
    private backendService: BackendService,
    private location: Location
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

    this.backendService.getChannels().subscribe(response => {
      this.channelData = response;
      // this.channelData.channels.unshift.apply(this.channelData.channels, [{ name: "all", id: "all" }])
      const channel = this.route.snapshot.paramMap.get('channel');
      if (channel == undefined) {
        this.channelNotFound = true;
        this.dataLoaded = true;
      }
      else {
        this.selectChannel(this.route.snapshot.paramMap.get('channel') ?? '');
      }
    });
  }

  private selectChannel(channelId: string): void {
    let channel = this.findChannel(channelId);
    if (channel == undefined) {
      this.channelNotFound = true;
      this.dataLoaded = true;
      return;
    }

    this.loadData(channel);
  }

  private loadData(channel: Channel) {
    this.selectedChannel = channel;
    this.dataLoaded = false;
    // TODO implement filters

    const page = (this.first / this.rows) + 1;
    this.backendService.getMessages(channel, page, this.rows, "desc", [], "", "", true).subscribe(response => {
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
          })
        });
      });
      this.dataLoaded = true;
      this.updateUrl();
    });
  }

  private findChannel(channelId: string): Channel | undefined {
    return this.channelData.channels.find((item) => { return item.id == channelId } );
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
    this.reloadData()
  }

  reloadData() {
    this.loadData(this.selectedChannel);
  }

  private updateUrl(): void {
    let url = '/gallery/' + this.selectedChannel.id;

    let parameters: string[] = [];
    // TODO
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

      this.reloadData();
    }
  }
}
