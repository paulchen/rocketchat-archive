import {Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {Channel, ChannelData, ChannelStats} from "./channel-data";
import {MessageData, MessagePage} from "./message-data";
import {UserData} from "./user-data";
import {VersionData} from "./version-data";
import {ReportData} from "./report-data";

@Injectable({
  providedIn: 'root'
})
export class BackendService {
  constructor(private http: HttpClient) { }

  getChannels(): Observable<ChannelData> {
    return this.http.get<ChannelData>("./services/channels");
  }

  getMessages(channel: Channel, page: number, limit: number, sort: string, userIds: string[], message: string, date: string): Observable<MessageData> {
    const params = { page: page, limit: limit, sort: sort, userIds: userIds.join(","), text: message, date: date };
    return this.http.get<MessageData>("./services/channels/" + encodeURIComponent(channel.id) + "/messages", { params });
  }

  getUsers(): Observable<UserData> {
    return this.http.get<UserData>("./services/users")
  }

  getVersion(): Observable<VersionData> {
    return this.http.get<VersionData>("./services/version")
  }

  getMessage(channel: string, message: string): Observable<MessagePage> {
    return this.http.get<MessagePage>("./services/channels/" + encodeURIComponent(channel) + "/messages/" + encodeURIComponent(message));
  }

  getChannelStats(channel: Channel, startDate: string, endDate: string ): Observable<ChannelStats>{
    let params = { startDate: startDate, endDate: endDate };
    return this.http.get<ChannelStats>("./services/channels/" + encodeURIComponent(channel.id) + "/stats", { params })
  }

  getReports(page: number, limit: number, sort: string): Observable<ReportData> {
    const params = { page: page, limit: limit, sort: sort };
    return this.http.get<ReportData>("./services/reports", { params });
  }
}


