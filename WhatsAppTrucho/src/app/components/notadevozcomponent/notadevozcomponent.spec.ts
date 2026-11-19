import { ComponentFixture, TestBed } from "@angular/core/testing";
import { Notadevozcomponent } from "./notadevozcomponent";

describe("Notadevozcomponent", () => {
  let component: Notadevozcomponent;
  let fixture: ComponentFixture<Notadevozcomponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [Notadevozcomponent],
    }).compileComponents();

    fixture = TestBed.createComponent(Notadevozcomponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });
});
