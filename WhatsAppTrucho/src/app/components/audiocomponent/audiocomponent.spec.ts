import { ComponentFixture, TestBed } from "@angular/core/testing";
import { Audiocomponent } from "./audiocomponent";

describe("Audiocomponent", () => {
  let component: Audiocomponent;
  let fixture: ComponentFixture<Audiocomponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [Audiocomponent],
    }).compileComponents();

    fixture = TestBed.createComponent(Audiocomponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });
});
