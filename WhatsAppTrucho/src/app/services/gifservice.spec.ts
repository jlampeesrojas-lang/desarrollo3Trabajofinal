import { TestBed } from "@angular/core/testing";
import { Gifservice } from "./gifservice";

describe("Gifservice", () => {
  let service: Gifservice;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(Gifservice);
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });
});
