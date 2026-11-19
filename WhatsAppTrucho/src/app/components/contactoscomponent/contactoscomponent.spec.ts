import { ComponentFixture, TestBed } from "@angular/core/testing";
import { Contactoscomponent } from "./contactoscomponent";

describe("Contactoscomponent", () => {
  let component: Contactoscomponent;
  let fixture: ComponentFixture<Contactoscomponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [Contactoscomponent],
    }).compileComponents();

    fixture = TestBed.createComponent(Contactoscomponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });
});
