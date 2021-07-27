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
