export class MessageData {
  messages: Message[];
  messageCount: number;
}

export class Message {
  id: string;
  message: string;
  timestamp: Date;
  username: string;
}

export class MessagePage {
  channel: string;
  message: string;
  page: number;
}
