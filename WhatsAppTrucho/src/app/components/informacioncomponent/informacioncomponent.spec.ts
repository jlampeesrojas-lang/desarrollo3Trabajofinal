import { ComponentFixture, TestBed } from "@angular/core/testing";
import { Informacioncomponent } from "./informacioncomponent";

describe("Informacioncomponent", () => {
  let component: Informacioncomponent;
  let fixture: ComponentFixture<Informacioncomponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [Informacioncomponent],
    }).compileComponents();

    fixture = TestBed.createComponent(Informacioncomponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });
});
