export class MessageData {
  messages: Message[];
  messageCount: number;
}

export class Message {
  id: string;
  rid: string;
  message: string;
  timestamp: Date;
  username: string;
  attachments: Attachment[];
  editedAt: Date;
  editedBy: string;
}

export class Attachment {
  type: string;
  title: string;
  titleLink: string;
  description: string;
}

export class MessagePage {
  channel: string;
  message: string;
  page: number;
}

export class MessageHistory {
  channel: string;
  message: string;
  history: Message[];
}

