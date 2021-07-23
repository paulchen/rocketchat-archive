import {Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {Channel, ChannelData} from "./channel-data";
import {MessageData} from "./message-data";

@Injectable({
  providedIn: 'root'
})
export class BackendService {
  constructor(private http: HttpClient) { }

  getChannels(): Observable<ChannelData> {
    return this.http.get<ChannelData>("./services/channels");
  }

  getMessages(channel: Channel, page: number): Observable<MessageData> {
    return this.http.get<MessageData>("./services/channels/" + channel.id + "/messages?page=" + page);
  }
}


