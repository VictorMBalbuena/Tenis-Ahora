import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { CustomCheckbox } from "../custom-checkbox";

describe("CustomCheckbox", () => {
  it("muestra el label y refleja el estado marcado", () => {
    render(<CustomCheckbox label="Techada" name="techada" checked onChange={vi.fn()} />);
    expect(screen.getByText("Techada")).toBeInTheDocument();
    expect(screen.getByRole("checkbox")).toBeChecked();
  });

  it("al tocarlo dispara onChange", async () => {
    const onChange = vi.fn();
    render(<CustomCheckbox label="Techada" name="techada" checked={false} onChange={onChange} />);

    await userEvent.click(screen.getByRole("checkbox"));
    expect(onChange).toHaveBeenCalled();
  });
});
