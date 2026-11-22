import { TestBed } from "@angular/core/testing";
import { Bloqueochatservice } from "./bloqueochatservice";

describe("Bloqueochatservice", () => {
  let service: Bloqueochatservice;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(Bloqueochatservice);
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });
});
