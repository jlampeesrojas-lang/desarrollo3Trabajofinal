import { ComponentFixture, TestBed } from "@angular/core/testing";
import { Confirmaremailcomponent } from "./confirmaremailcomponent";

describe("Confirmaremailcomponent", () => {
  let component: Confirmaremailcomponent;
  let fixture: ComponentFixture<Confirmaremailcomponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [Confirmaremailcomponent],
    }).compileComponents();

    fixture = TestBed.createComponent(Confirmaremailcomponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });
});
