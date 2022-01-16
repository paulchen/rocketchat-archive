import {Message} from "./message-data";

export class ReportData {
  reports: Report[];
  reportCount: number;
}

export class Report {
  id: string;
  message: Message;
  description: string;
  timestamp: Date;
  reporter: User;
}

export class User {
  id: string;
  name: string;
  username: string;
}
