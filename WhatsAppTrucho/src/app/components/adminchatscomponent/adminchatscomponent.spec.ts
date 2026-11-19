import { ComponentFixture, TestBed } from "@angular/core/testing";
import { Adminchatscomponent } from "./adminchatscomponent";

describe("Adminchatscomponent", () => {
  let component: Adminchatscomponent;
  let fixture: ComponentFixture<Adminchatscomponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [Adminchatscomponent],
    }).compileComponents();

    fixture = TestBed.createComponent(Adminchatscomponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });
});
