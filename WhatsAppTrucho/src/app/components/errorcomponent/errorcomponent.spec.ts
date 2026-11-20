import { ComponentFixture, TestBed } from "@angular/core/testing";
import { Errorcomponent } from "./errorcomponent";

describe("Errorcomponent", () => {
  let component: Errorcomponent;
  let fixture: ComponentFixture<Errorcomponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [Errorcomponent],
    }).compileComponents();

    fixture = TestBed.createComponent(Errorcomponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });
});
