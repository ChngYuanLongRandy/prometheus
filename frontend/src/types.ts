export interface TrackedUrl {
  id: string;
  url: string;
  label: string | null;
  createdAt: string;
  lastCheckedAt: string | null;
  lastStatus: string; // pending | ok | changed | unchanged | error
  lastMethod: string | null;
  lastError: string | null;
}

export interface UpdateItem {
  id: string;
  urlId: string;
  url: string;
  label: string | null;
  diff: string;
  addedLines: number;
  removedLines: number;
  detectedAt: string;
}

export interface CheckResult {
  outcome: string; // FIRST_SNAPSHOT | CHANGED | UNCHANGED | ERROR
  url: TrackedUrl;
}
