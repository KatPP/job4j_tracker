package ru.job4j.action;

import ru.job4j.tracker.Input;
import ru.job4j.tracker.Item;
import ru.job4j.tracker.Output;
import ru.job4j.tracker.Store;

import java.util.List;
public class FindAll implements UserAction {
    private final Output out;

    public FindAll(Output output) {
        this.out = output;
    }

    @Override
    public String name() {
        return "Показать все заявки";
    }

    @Override
    public boolean execute(Input input, Store tracker) {
        out.println("=== Вывод всех заявок ===");
        List<Item> items = tracker.findAll();
        if (items.size() > 0) {
            for (Item item : items) {
                out.println(item);
            }
        } else {
            out.println("Хранилище еще не содержит заявок");
        }
        return true;
    }
}