export class MessageData {
  messages: Message[];
  pages: number;
}

export class Message {
  id: string;
  message: string;
  timestamp: Date;
  username: string;
}
