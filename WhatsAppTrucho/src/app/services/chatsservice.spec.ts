import { TestBed } from "@angular/core/testing";
import { Chatsservice } from "./chatsservice";

describe("Chatsservice", () => {
  let service: Chatsservice;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(Chatsservice);
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });
});
