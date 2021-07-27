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

  getMessages(channel: Channel, page: number, limit: number, sort: string, username: string, message: string): Observable<MessageData> {
    const params = { page: page, limit: limit, sort: sort, user: username, text: message };
    return this.http.get<MessageData>("./services/channels/" + channel.id + "/messages", { params });
  }
}


