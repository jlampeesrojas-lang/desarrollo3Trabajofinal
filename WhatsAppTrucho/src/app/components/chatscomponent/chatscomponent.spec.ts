import { ComponentFixture, TestBed } from "@angular/core/testing";
import { Chatscomponent } from "./chatscomponent";

describe("Chatscomponent", () => {
  let component: Chatscomponent;
  let fixture: ComponentFixture<Chatscomponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [Chatscomponent],
    }).compileComponents();

    fixture = TestBed.createComponent(Chatscomponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });
});
