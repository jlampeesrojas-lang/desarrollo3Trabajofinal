import { ComponentFixture, TestBed } from "@angular/core/testing";
import { Notavozcomponent } from "./notavozcomponent";

describe("Notavozcomponent", () => {
  let component: Notavozcomponent;
  let fixture: ComponentFixture<Notavozcomponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [Notavozcomponent],
    }).compileComponents();

    fixture = TestBed.createComponent(Notavozcomponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });
});
